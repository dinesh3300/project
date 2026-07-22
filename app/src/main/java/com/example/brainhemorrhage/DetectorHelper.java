package com.example.brainhemorrhage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DetectorHelper {
    private static final String TAG = "DetectorHelper";
    private static final String MODEL_FILE = "hemorrhage_detector.tflite";
    private static final float CONFIDENCE_THRESHOLD = 0.60f;
    private static final float IOU_THRESHOLD = 0.25f;

    private Interpreter interpreter;
    private int inputSize;
    
    public static class DetectorResult {
        public Uri processedImageUri;
        public boolean hasHemorrhage;
        public float highestConfidence;
        public int detectionCount;
    }

    private static class Box {
        float x, y, w, h, conf;
        public Box(float x, float y, float w, float h, float conf) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.conf = conf;
        }
    }

    public DetectorHelper(Context context) throws IOException {
        MappedByteBuffer modelFile = FileUtil.loadMappedFile(context, MODEL_FILE);
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        interpreter = new Interpreter(modelFile, options);

        int[] inputShape = interpreter.getInputTensor(0).shape();
        inputSize = inputShape[1];
    }

    public DetectorResult detect(Context context, Bitmap originalBitmap) throws IOException {
        Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(mutableBitmap, inputSize, inputSize, true);

        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4);
        inputBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int val = intValues[pixel++];
                inputBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
                inputBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
                inputBuffer.putFloat((val & 0xFF) / 255.0f);
            }
        }

        int[] outputShape = interpreter.getOutputTensor(0).shape();
        float[][][] outputArr = new float[outputShape[0]][outputShape[1]][outputShape[2]];
        interpreter.run(inputBuffer, outputArr);

        int dim1 = outputShape[1];
        int dim2 = outputShape[2];
        boolean isTransposed = dim1 < dim2;
        int numBoxes = isTransposed ? dim2 : dim1;
        int numElements = isTransposed ? dim1 : dim2;
        
        boolean hasObjectness = false;
        int classes;
        if (numElements > 5 && (numElements % 2 != 0 || numElements < 10)) {
             if (numElements == 5) {
                 hasObjectness = false;
                 classes = 1;
             } else {
                 hasObjectness = true;
                 classes = numElements - 5;
             }
        } else {
             hasObjectness = false;
             classes = numElements - 4;
        }

        List<Box> boxes = new ArrayList<>();
        float maxConfSeen = 0f;

        for (int i = 0; i < numBoxes; i++) {
            float cx, cy, w, h, conf;
            
            if (isTransposed) {
                cx = outputArr[0][0][i];
                cy = outputArr[0][1][i];
                w = outputArr[0][2][i];
                h = outputArr[0][3][i];
                
                float maxClassProb = 0f;
                for (int c = 0; c < classes; c++) {
                    float prob = outputArr[0][hasObjectness ? 5 + c : 4 + c][i];
                    if (prob > maxClassProb) {
                        maxClassProb = prob;
                    }
                }
                conf = hasObjectness ? (outputArr[0][4][i] * maxClassProb) : maxClassProb;
            } else {
                cx = outputArr[0][i][0];
                cy = outputArr[0][i][1];
                w = outputArr[0][i][2];
                h = outputArr[0][i][3];
                
                float maxClassProb = 0f;
                for (int c = 0; c < classes; c++) {
                    float prob = outputArr[0][i][hasObjectness ? 5 + c : 4 + c];
                    if (prob > maxClassProb) {
                        maxClassProb = prob;
                    }
                }
                conf = hasObjectness ? (outputArr[0][i][4] * maxClassProb) : maxClassProb;
            }

            if (conf > maxConfSeen) maxConfSeen = conf;

            if (conf > CONFIDENCE_THRESHOLD) {
                boxes.add(new Box(cx, cy, w, h, conf));
            }
        }

        Collections.sort(boxes, (b1, b2) -> Float.compare(b2.conf, b1.conf));
        List<Box> nmsBoxes = new ArrayList<>();
        while (!boxes.isEmpty()) {
            Box bestBox = boxes.remove(0);
            nmsBoxes.add(bestBox);
            List<Box> remainingBoxes = new ArrayList<>();
            for (Box box : boxes) {
                if (iou(bestBox, box) < IOU_THRESHOLD) {
                    remainingBoxes.add(box);
                }
            }
            boxes = remainingBoxes;
        }
        
        if (nmsBoxes.size() > 1) {
            Box absoluteBest = nmsBoxes.get(0);
            nmsBoxes.clear();
            nmsBoxes.add(absoluteBest);
        }

        DetectorResult result = new DetectorResult();
        result.hasHemorrhage = !nmsBoxes.isEmpty();
        result.highestConfidence = maxConfSeen;
        result.detectionCount = nmsBoxes.size();

        if (!nmsBoxes.isEmpty()) {
            result.highestConfidence = nmsBoxes.get(0).conf;
            
            Canvas canvas = new Canvas(mutableBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8.0f);
            
            Paint textPaint = new Paint();
            textPaint.setColor(Color.RED);
            textPaint.setTextSize(60f);
            textPaint.setFakeBoldText(true);

            float scaleX = (float) mutableBitmap.getWidth() / inputSize;
            float scaleY = (float) mutableBitmap.getHeight() / inputSize;

            Box b = nmsBoxes.get(0);
            float left = (b.x - b.w / 2) * scaleX;
            float top = (b.y - b.h / 2) * scaleY;
            float right = (b.x + b.w / 2) * scaleX;
            float bottom = (b.y + b.h / 2) * scaleY;
            canvas.drawRect(left, top, right, bottom, paint);
            canvas.drawText(String.format("Hemorrhage: %.0f%%", b.conf * 100), left, top - 10, textPaint);
        }

        File tempFile = new File(context.getCacheDir(), "processed_det_" + System.currentTimeMillis() + ".jpg");
        FileOutputStream out = new FileOutputStream(tempFile);
        mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        out.close();

        result.processedImageUri = Uri.fromFile(tempFile);
        return result;
    }

    private float iou(Box a, Box b) {
        float aLeft = a.x - a.w / 2;
        float aTop = a.y - a.h / 2;
        float aRight = a.x + a.w / 2;
        float aBottom = a.y + a.h / 2;

        float bLeft = b.x - b.w / 2;
        float bTop = b.y - b.h / 2;
        float bRight = b.x + b.w / 2;
        float bBottom = b.y + b.h / 2;

        float interLeft = Math.max(aLeft, bLeft);
        float interTop = Math.max(aTop, bTop);
        float interRight = Math.min(aRight, bRight);
        float interBottom = Math.min(aBottom, bBottom);

        if (interRight < interLeft || interBottom < interTop) return 0.0f;

        float interArea = (interRight - interLeft) * (interBottom - interTop);
        float unionArea = (a.w * a.h) + (b.w * b.h) - interArea;

        return interArea / unionArea;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
