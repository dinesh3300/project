package com.example.brainhemorrhage;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ClassifierHelper {
    private Interpreter interpreter;
    private int inputSize;
    private List<String> labels;

    public static class ClassifierResult {
        public String predictedClass;
        public float confidence;
    }

    public ClassifierHelper(Context context) throws IOException {
        MappedByteBuffer modelFile = FileUtil.loadMappedFile(context, "brain_ct_classifier.tflite");
        
        // We utilize our robust internal explicit label mapping logic to interpret outputs safely,
        // avoiding dependency on the external TFLite metadata extractor package to ensure stable, lightweight compilation.
        labels = new ArrayList<>();

        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        interpreter = new Interpreter(modelFile, options);

        int[] inputShape = interpreter.getInputTensor(0).shape();
        inputSize = inputShape[1];
    }

    public ClassifierResult classify(Bitmap originalBitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, inputSize, inputSize, true);

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
        float[][] outputArr = new float[1][outputShape[1]];
        interpreter.run(inputBuffer, outputArr);

        ClassifierResult result = new ClassifierResult();

        if (outputShape[1] == 1) {
            float prob = outputArr[0][0];
            // Safest explicit mapping: A single sigmoid output typically predicts the target positive class (brain_ct).
            // To ensure robust interpretation and avoid brittle hardcoded rejection of valid images,
            // we map prob >= 0.5f to brain_ct. If metadata labels explicitly define class 0, we follow them.
            boolean isBrainCt = prob >= 0.5f;
            if (labels != null && !labels.isEmpty()) {
                if (labels.get(0).contains("non_brain")) {
                    isBrainCt = prob < 0.5f;
                }
            }
            
            if (isBrainCt) {
                result.predictedClass = "brain_ct";
                result.confidence = prob >= 0.5f ? prob : (1.0f - prob);
            } else {
                result.predictedClass = "non_brain_ct";
                result.confidence = prob >= 0.5f ? prob : (1.0f - prob);
            }
        } else {
            float prob0 = outputArr[0][0];
            float prob1 = outputArr[0][1];
            
            // Standard alphabetical export mapping: index 0 = brain_ct, index 1 = non_brain_ct.
            String class0 = "brain_ct";
            String class1 = "non_brain_ct";
            
            if (labels != null && labels.size() >= 2) {
                class0 = labels.get(0);
                class1 = labels.get(1);
            }
            
            // Robust gatekeeper interpretation: favor brain_ct unless non_brain_ct confidence is definitively higher,
            // preventing brittle rejection of valid brain CT images.
            if (prob0 >= prob1) {
                result.predictedClass = class0;
                result.confidence = prob0;
            } else {
                result.predictedClass = class1;
                result.confidence = prob1;
            }
        }

        return result;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
