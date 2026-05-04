package com.GestionInscripcionCursos.servicios;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class ArchivoServicio {

    private final Cloudinary cloudinary;

    // Actualizamos el constructor para leer tus 3 variables separadas
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

    public String subirArchivo(MultipartFile archivo) throws IOException {
        // "auto" detecta automáticamente si es imagen, pdf, word (raw), etc.
        Map parametros = ObjectUtils.asMap(
                "resource_type", "auto",
                "folder", "chat_archivos"
        );

        Map resultado = cloudinary.uploader().upload(archivo.getBytes(), parametros);
        return resultado.get("secure_url").toString();
    }
}