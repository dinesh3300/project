package com.example.brainhemorrhage;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;

public class SubtypeClassifierHelper {
    private static final String MODEL_FILE = "Hemorrhage.tflite";
    private Interpreter interpreter;
    private int inputSize = 224;

    public static class SubtypeResult {
        public float intraventricular;
        public float intraparenchymal;
        public float subarachnoid;
        public float epidural;
        public float subdural;
        
        public boolean hasSubtypeHemorrhage() {
            return intraventricular >= 0.5f || 
                   intraparenchymal >= 0.5f || 
                   subarachnoid >= 0.5f || 
                   epidural >= 0.5f || 
                   subdural >= 0.5f;
        }

        public String getTopSubtype() {
            float[] probs = {intraventricular, intraparenchymal, subarachnoid, epidural, subdural};
            String[] names = {"Intraventricular", "Intraparenchymal", "Subarachnoid", "Epidural", "Subdural"};
            
            int maxIdx = 0;
            for (int i = 1; i < probs.length; i++) {
                if (probs[i] > probs[maxIdx]) {
                    maxIdx = i;
                }
            }
            return names[maxIdx] + " (" + String.format("%.1f%%", probs[maxIdx] * 100) + ")";
        }
    }

    public SubtypeClassifierHelper(Context context) {
        try {
            MappedByteBuffer modelFile = FileUtil.loadMappedFile(context, MODEL_FILE);
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            interpreter = new Interpreter(modelFile, options);
        } catch (Exception e) {
            e.printStackTrace();
            interpreter = null;
        }
    }

    public SubtypeResult classifySubtypes(Bitmap originalBitmap) {
        SubtypeResult result = new SubtypeResult();
        if (interpreter == null) {
            result.intraventricular = 0.0f;
            result.intraparenchymal = 0.0f;
            result.subarachnoid = 0.0f;
            result.epidural = 0.0f;
            result.subdural = 0.0f;
            return result;
        }
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, inputSize, inputSize, true);

        // Input shape: [1, 224, 224, 3] float32. Size = 1 * 224 * 224 * 3 * 4 bytes
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4);
        inputBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int val = intValues[pixel++];
                
                // Convert pixel to grayscale value in range [0.0f, 1.0f]
                float r = ((val >> 16) & 0xFF) / 255.0f;
                float g = ((val >> 8) & 0xFF) / 255.0f;
                float b = (val & 0xFF) / 255.0f;
                float gray = 0.299f * r + 0.587f * g + 0.114f * b;
                
                // Channel 0, 1, 2 all copy gray (matching the single-window fallback of the training dataset)
                inputBuffer.putFloat(gray);
                inputBuffer.putFloat(gray);
                inputBuffer.putFloat(gray);
            }
        }

        // Prepare inputs map
        java.util.Map<String, Object> inputs = new java.util.HashMap<>();
        inputs.put("x", inputBuffer);

        // Prepare outputs map
        java.util.Map<String, Object> outputs = new java.util.HashMap<>();
        float[][] outputArr = new float[1][5];
        outputs.put("probs", outputArr);

        // Run inference using the "infer" signature definition for this On-Device Training model
        interpreter.runSignature(inputs, outputs, "infer");

        result = new SubtypeResult();
        result.intraventricular = outputArr[0][0];
        result.intraparenchymal = outputArr[0][1];
        result.subarachnoid = outputArr[0][2];
        result.epidural = outputArr[0][3];
        result.subdural = outputArr[0][4];

        return result;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
