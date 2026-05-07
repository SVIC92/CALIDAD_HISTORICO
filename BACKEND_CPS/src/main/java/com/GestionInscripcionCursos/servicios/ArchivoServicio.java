package com.GestionInscripcionCursos.servicios;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class ArchivoServicio {
    private final Cloudinary cloudinary;

    public ArchivoServicio(
            @Value("${cloudinary.cloud_name:}") String cloudName,
            @Value("${cloudinary.api_key:}") String apiKey,
            @Value("${cloudinary.api_secret:}") String apiSecret) {
                 
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    public String subirArchivo(MultipartFile archivo, String carpeta) throws IOException {
        // 1. Crear un archivo temporal con el nombre original para conservar la extensión (ej. .docx)
        File archivoTemporal = convertirAFile(archivo);

        try {
            // 2. Subir a Cloudinary indicando que conserve el nombre y formato
            Map parametros = ObjectUtils.asMap(
                    "resource_type", "auto",
                    "folder", carpeta != null ? carpeta : "general",
                    "use_filename", true,
                    "unique_filename", true
            );
            
            Map resultado = cloudinary.uploader().upload(archivoTemporal, parametros);
            return resultado.get("secure_url").toString();
        } finally {
            // 3. Eliminar el archivo temporal del servidor para no consumir espacio
            if (archivoTemporal.exists()) {
                archivoTemporal.delete();
            }
        }
    }

    private File convertirAFile(MultipartFile archivo) throws IOException {
        // Usamos el directorio temporal del sistema operativo
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + archivo.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(archivo.getBytes());
        }
        return convFile;
    }
}