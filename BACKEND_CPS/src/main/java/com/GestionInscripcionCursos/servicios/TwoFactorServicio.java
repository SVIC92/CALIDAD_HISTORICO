package com.GestionInscripcionCursos.servicios;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.net.URLEncoder;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwoFactorServicio {

    private static final long TIME_STEP_SECONDS = 30L;
    private static final int CODE_DIGITS = 6;
    private static final int VALIDATION_WINDOW = 1;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base32 base32 = new Base32();

    @Value("${app.2fa.issuer:GCIPlus}")
    private String issuer;

    public String generarSecreto() {
        byte[] buffer = new byte[20];
        secureRandom.nextBytes(buffer);
        return base32.encodeToString(buffer).replace("=", "");
    }

    public String construirOtpAuthUrl(String email, String secreto) {
        String label = urlEncode(issuer + ":" + email);
        String issuerEncoded = urlEncode(issuer);
        return "otpauth://totp/" + label
                + "?secret=" + secreto
                + "&issuer=" + issuerEncoded
                + "&algorithm=SHA1&digits=" + CODE_DIGITS
                + "&period=" + TIME_STEP_SECONDS;
    }

    public boolean validarCodigo(String secreto, String codigo) {
        if (secreto == null || secreto.isBlank() || codigo == null || !codigo.matches("\\d{6}")) {
            return false;
        }

        long timeIndex = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;

        for (int i = -VALIDATION_WINDOW; i <= VALIDATION_WINDOW; i++) {
            String expected = generarCodigoTotp(secreto, timeIndex + i);
            if (codigo.equals(expected)) {
                return true;
            }
        }
        return false;
    }

    public byte[] generarQrPng(String otpAuthUrl, int size) {
        try {
            int qrSize = Math.max(180, Math.min(size, 600));
            BitMatrix bitMatrix = new com.google.zxing.MultiFormatWriter()
                    .encode(otpAuthUrl, BarcodeFormat.QR_CODE, qrSize, qrSize);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | java.io.IOException ex) {
            return new byte[0];
        }
    }

    private String generarCodigoTotp(String secreto, long timeIndex) {
        try {
            byte[] key = base32.decode(secreto);
            byte[] data = new byte[8];
            long value = timeIndex;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (value & 0xFF);
                value >>= 8;
            }

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            return "";
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
