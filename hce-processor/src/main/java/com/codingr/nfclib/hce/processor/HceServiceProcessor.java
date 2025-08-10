package com.codingr.nfclib.hce.processor;

import com.codingr.nfclib.hce.annotations.ApduController;
import com.codingr.nfclib.hce.annotations.HceService;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@AutoService(Processor.class)
@SupportedAnnotationTypes({
    "com.codingr.nfclib.hce.annotations.HceService",
    "com.codingr.nfclib.hce.annotations.ApduController"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class HceServiceProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<String> allAids = new HashSet<>();
        List<String> serviceClasses = new ArrayList<>();
        
        // Process @HceService annotations
        for (Element element : roundEnv.getElementsAnnotatedWith(HceService.class)) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                HceService hceService = typeElement.getAnnotation(HceService.class);
                
                for (String aid : hceService.aids()) {
                    allAids.add(aid);
                }
                serviceClasses.add(typeElement.getQualifiedName().toString());
            }
        }
        
        // Process @ApduController annotations to collect AIDs
        for (Element element : roundEnv.getElementsAnnotatedWith(ApduController.class)) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                ApduController controller = typeElement.getAnnotation(ApduController.class);
                
                for (String aid : controller.aids()) {
                    allAids.add(aid);
                }
            }
        }
        
        // Only generate files if we found controllers or services
        if (!allAids.isEmpty()) {
            try {
                generateAidListXml(allAids.toArray(new String[0]));
                generateAndroidManifestFragment();
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, 
                    "Could not generate HCE files: " + e.getMessage());
            }
        }
        
        return true;
    }

    private void generateAidListXml(String[] aids) throws IOException {
        FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "res/xml/aid_list.xml");
        try (Writer writer = file.openWriter()) {
            writer.write("<host-apdu-service xmlns:android=\"http://schemas.android.com/apk/res/android\"\n");
            writer.write("    android:description=\"@string/servicedesc\"\n");
            writer.write("    android:requireDeviceUnlock=\"false\">\n");
            for (String aid : aids) {
                writer.write("    <aid-group android:description=\"@string/aiddescription\"\n");
                writer.write("        android:category=\"other\">\n");
                writer.write("        <aid-filter android:name=\"" + aid + "\"/>\n");
                writer.write("    </aid-group>\n");
            }
            writer.write("</host-apdu-service>\n");
        }
    }

    private void generateAndroidManifestFragment() throws IOException {
        FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "AndroidManifest.xml");
        try (Writer writer = file.openWriter()) {
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            writer.write("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n");
            writer.write("\n");
            writer.write("    <!-- NFC permissions automatically added by HCE library -->\n");
            writer.write("    <uses-permission android:name=\"android.permission.NFC\" />\n");
            writer.write("\n");
            writer.write("    <!-- NFC hardware requirements -->\n");
            writer.write("    <uses-feature\n");
            writer.write("        android:name=\"android.hardware.nfc\"\n");
            writer.write("        android:required=\"true\" />\n");
            writer.write("    <uses-feature\n");
            writer.write("        android:name=\"android.hardware.nfc.hce\"\n");
            writer.write("        android:required=\"true\" />\n");
            writer.write("\n");
            writer.write("    <application>\n");
            writer.write("        <!-- HCE Service automatically registered by annotation processor -->\n");
            writer.write("        <service\n");
            writer.write("            android:name=\"com.codingr.nfclib.hce.core.ApduRouterService\"\n");
            writer.write("            android:exported=\"true\"\n");
            writer.write("            android:permission=\"android.permission.BIND_NFC_SERVICE\">\n");
            writer.write("            <intent-filter>\n");
            writer.write("                <action android:name=\"android.nfc.cardemulation.action.HOST_APDU_SERVICE\" />\n");
            writer.write("            </intent-filter>\n");
            writer.write("            <meta-data\n");
            writer.write("                android:name=\"android.nfc.cardemulation.host_apdu_service\"\n");
            writer.write("                android:resource=\"@xml/aid_list\" />\n");
            writer.write("        </service>\n");
            writer.write("    </application>\n");
            writer.write("</manifest>\n");
        }
    }
}
