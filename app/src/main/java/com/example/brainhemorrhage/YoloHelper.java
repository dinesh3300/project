package com.example.brainhemorrhage;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.IOException;

public class YoloHelper {
    private static final String TAG = "YoloHelper";

    public static class YoloResult {
        public Uri processedImageUri;
        public boolean hasHemorrhage;
        public float highestConfidence;
        public int detectionCount;
        public boolean validationFailed;
        public String validationError;

        // Subtype classification probabilities from Hemorrhage.tflite
        public float intraventricular;
        public float intraparenchymal;
        public float subarachnoid;
        public float epidural;
        public float subdural;
    }

    private ClassifierHelper classifierHelper;
    private DetectorHelper detectorHelper;
    private SubtypeClassifierHelper subtypeClassifierHelper;

    public YoloHelper(Context context) throws IOException {
        // Load only the classifier initially to save startup memory/time
        classifierHelper = new ClassifierHelper(context);
    }

    public YoloResult detect(Context context, Uri imageUri) throws IOException {
        Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
        
        // Stage 1: Classifier as Gatekeeper
        ClassifierHelper.ClassifierResult clsResult = classifierHelper.classify(originalBitmap);
        if ("non_brain_ct".equals(clsResult.predictedClass)) {
            YoloResult result = new YoloResult();
            result.validationFailed = true;
            result.validationError = "Input rejected: not a brain CT image";
            return result;
        }
        
        // Stage 2: Detector for Hemorrhage (Lazy loading)
        if (detectorHelper == null) {
            detectorHelper = new DetectorHelper(context);
        }
        DetectorHelper.DetectorResult detResult = detectorHelper.detect(context, originalBitmap);
        
        YoloResult result = new YoloResult();
        result.processedImageUri = detResult.processedImageUri;
        result.validationFailed = false;

        if (detResult.hasHemorrhage) {
            // Stage 3: Subtype Classifier (Lazy loading)
            if (subtypeClassifierHelper == null) {
                subtypeClassifierHelper = new SubtypeClassifierHelper(context);
            }
            SubtypeClassifierHelper.SubtypeResult subResult = subtypeClassifierHelper.classifySubtypes(originalBitmap);
            
            result.hasHemorrhage = true;
            result.highestConfidence = detResult.highestConfidence;
            result.detectionCount = detResult.detectionCount;
            
            // Populate subtype probabilities
            result.intraventricular = subResult.intraventricular;
            result.intraparenchymal = subResult.intraparenchymal;
            result.subarachnoid = subResult.subarachnoid;
            result.epidural = subResult.epidural;
            result.subdural = subResult.subdural;
        } else {
            result.hasHemorrhage = false;
            result.highestConfidence = detResult.highestConfidence;
            result.detectionCount = 0;
            
            // Populate empty subtype probabilities
            result.intraventricular = 0.0f;
            result.intraparenchymal = 0.0f;
            result.subarachnoid = 0.0f;
            result.epidural = 0.0f;
            result.subdural = 0.0f;
        }
        
        return result;
    }

    public void close() {
        if (classifierHelper != null) {
            classifierHelper.close();
            classifierHelper = null;
        }
        if (detectorHelper != null) {
            detectorHelper.close();
            detectorHelper = null;
        }
        if (subtypeClassifierHelper != null) {
            subtypeClassifierHelper.close();
            subtypeClassifierHelper = null;
        }
    }
}
