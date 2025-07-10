package com.example.hceprocessor;

import com.example.hcelibrary.annotations.HceService;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
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
@SupportedAnnotationTypes("com.example.hcelibrary.annotations.HceService")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class HceServiceProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(HceService.class)) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                HceService hceService = typeElement.getAnnotation(HceService.class);

                try {
                    generateAidListXml(hceService.aids());
                    generateAndroidManifestFragment(typeElement.getQualifiedName().toString(), hceService.description());
                } catch (IOException e) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Could not generate HCE files: " + e.getMessage());
                }
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

    private void generateAndroidManifestFragment(String serviceName, String serviceDescription) throws IOException {
        FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "AndroidManifest.xml");
        try (Writer writer = file.openWriter()) {
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            writer.write("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n");
            writer.write("    <application>\n");
            writer.write("        <service\n");
            writer.write("            android:name=\"" + serviceName + "\"\n");
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
