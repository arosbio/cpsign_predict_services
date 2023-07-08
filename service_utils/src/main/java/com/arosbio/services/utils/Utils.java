package com.arosbio.services.utils;


import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.ServiceLoader;

import javax.imageio.ImageIO;

import org.slf4j.Logger;

import com.arosbio.api.model.BadRequestError;
import com.arosbio.api.model.ErrorResponse;
import com.arosbio.encryption.EncryptionSpecification;
import com.arosbio.io.UriUtils;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class Utils {
	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Utils.class);
	private static final int MAX_NUM_STACK_TO_LOGG = 10;
	
	public static final String PNG_MEDIA_TYPE = "image/png";
	public static final String MODEL_FILE_ENV_VARIABLE = "MODEL_FILE";
	public static final String ENCRYPTION_KEY_ENV_VARIABLE = "ENCRYPTION_KEY";
	public static final String ENCRYPTION_KEY_FILE_ENV_VARIABLE = "ENCRYPTION_KEY_FILE";
	public static final String DEFAULT_MODEL_PATH = "/var/lib/jetty/model.jar";
	public static final int DEFAULT_IMAGE_WH = 600;
	public static final int MIN_IMAGE_SIZE = 100;
	public static final int MAX_IMAGE_SIZE = 5000;

	public static EncryptionSpecification getEncryptionKeyOrNull() throws IllegalArgumentException {
		ServiceLoader<EncryptionSpecification> loader = ServiceLoader.load(EncryptionSpecification.class);
		Iterator<EncryptionSpecification> serviceIterator = loader.iterator();

		EncryptionSpecification spec = null;
		if (!serviceIterator.hasNext()){
			LOGGER.debug("No encryption specification implementation available on classpath");
			return null;
		}

		// We had at least one specification - pick the first one
		spec = serviceIterator.next();
		LOGGER.debug("Loaded encryption spec implementation of type: {}", spec.getName());

		// Check if key given directly
		String keyOrNull = getProperty(ENCRYPTION_KEY_ENV_VARIABLE);
		if (keyOrNull != null){
			LOGGER.debug("Key given in plain text");
			try {
				spec.init(Base64.getDecoder().decode(keyOrNull));
				return spec;
			} catch (Exception e){
				LOGGER.debug("Failed setting up encryption specification",e);
				throw new IllegalArgumentException("Could not initialize encryption specification from the given key: " + e.getMessage());
			}
		}


		// no key given directly - check if given as file
		LOGGER.debug("No key given as plain text, try as a file");
		String keyFileOrNull = getProperty(ENCRYPTION_KEY_FILE_ENV_VARIABLE);
		if (keyFileOrNull != null){
			// Convert it to a URI
			URI keyURI = null;
			try {
				keyURI = UriUtils.getURI(keyFileOrNull);
				LOGGER.debug("Loading encryption key from input: {}\nconverted to URI: {}", keyFileOrNull,keyURI);
			} catch (Exception e){
				LOGGER.debug("Failed getting a URI from input: {}",keyFileOrNull);
				throw new IllegalArgumentException("Invalid encryption key file: " + keyFileOrNull);
			}
			
			// Read the bytes and init encryption spec
			byte[] key = null;
			try (
				InputStream stream = UriUtils.getInputStream(keyURI);
			){
				key = stream.readAllBytes();
				spec.init(key);
				return spec;
			} catch (IOException e){
				LOGGER.debug("Failed reading bytes from encryption file", e);
				throw new IllegalArgumentException("Failed reading encryption key from file");
			} catch (InvalidKeyException e) {
				LOGGER.debug("Failed setting up encryption spec with the given key", e);
				throw new IllegalArgumentException("Failed configuring encryption key: " + e.getMessage());
			} finally {
				if (key != null){
					Arrays.fill(key, (byte)0);
				}
			}
			
		}


		return null;
	}

	private static String getProperty(final String name){
		// First take environment variable if set 
		String envProp = System.getenv(name);
		if (envProp!=null && !envProp.isBlank()){
			return envProp;
		}
		// If not set - check system property (i.e. argument given to the JVM)
		String jvmProp = System.getProperty(name);
		if (jvmProp!=null && !jvmProp.isBlank()){
			return jvmProp;
		}
		return null;
	}

	public static String getModelURL(){
		// First take environment variable if set 
		String envProp = System.getenv(MODEL_FILE_ENV_VARIABLE);
		if (envProp!=null && !envProp.isBlank()){
			return envProp;
		}
			
		// If not set - check system property (i.e. argument given to the JVM)
		String jvmProp = System.getProperty(MODEL_FILE_ENV_VARIABLE);
		if (jvmProp!=null && !jvmProp.isBlank()){
			return jvmProp;
		}
		
		// Use the default path as backup
		return DEFAULT_MODEL_PATH;
	}

	public static byte[] convertToByteArray(BufferedImage image) throws IOException {
		try(
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			){
			ImageIO.write(image, "png", baos);
			return baos.toByteArray();
		}
	}

	public static String getStackTrace(Throwable e) {
		StringBuilder sb = new StringBuilder();
		StackTraceElement[] stack = e.getStackTrace();
		
		sb.append(e.getClass());
		sb.append(": ");
		sb.append(e.getMessage());
		for (int i=0; i<MAX_NUM_STACK_TO_LOGG && i<stack.length; i++) {
			sb.append('\n');
			sb.append('\t');
			sb.append(stack[i]);
		}
		
		return sb.toString();
	}
	
	public static String decodeURL(String text) throws MalformedURLException {
		if (text ==null || text.isEmpty())
			throw new IllegalArgumentException("Empty data");

		// Charges should be kept as charges, so we replace the input "+" with URL-encoding of a "+" instead
		if (text.contains("+")) {
			text = text.replace("+", "%2B");
		}

		// Clean the molecule-string from URL encoding
		try {
			return URLDecoder.decode(text, StandardCharsets.UTF_8.name());
		} catch (Exception e) {
			throw new MalformedURLException("Could not decode text");
		}
	}
	
	public static double roundTo3digits(double val){
		return Math.round(val*1000.0)/1000.0;
	}

	public static Response getResponse(ErrorResponse error) {
		return Response
			.status(error.getCode())
			.entity(error)
			.type(MediaType.APPLICATION_JSON)
			.build();
	}
	
	public static Response getEmptyImageResponse(int w, int h) {
		try {
			BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = image.createGraphics();
			g2d.setColor(Color.WHITE);
			g2d.fillRect(0, 0, w, h);
			g2d.dispose();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			byte[] imageData = baos.toByteArray();

			return Response.ok( new ByteArrayInputStream(imageData) ).build();
		} catch (IOException e) {
			LOGGER.info("Failed returning empty image for empty molecule input");
			return Utils.getResponse(new ErrorResponse(INTERNAL_SERVER_ERROR, "Server error"));
		}
	}
	
	/**
	 * Validate the size given the {@link MIN_IMAGE_SIZE} and {@link MAX_IMAGE_SIZE} 
	 * @param w
	 * @param h
	 * @return <code>null</code> if all good, or a error-Response if invalid input
	 */
	public static Response validateImageSize(int w, int h) {
		boolean validW = isValidSize(w);
		boolean validH = isValidSize(h);
		
		if (! validW && ! validH) {
			LOGGER.warn("Failing execution due to invalid image size, WxH: {}x{}", w, h);
			return getResponse(
					new BadRequestError(BAD_REQUEST, 
							String.format("Invalid imageWidth {%d} and imageHeight {%d}, both must be in the range [%d..%d]",
									w,h,MIN_IMAGE_SIZE,MAX_IMAGE_SIZE), Arrays.asList("imageWidth", "imageHeight")));
		} else if (! validW) {
			LOGGER.warn("Failing execution due to invalid image size, WxH: {}x{}", w, h);
			return getResponse(
					new BadRequestError(BAD_REQUEST, 
							String.format("Invalid imageWidth {%d}, value must be in the range [%d..%d]",
									w,MIN_IMAGE_SIZE,MAX_IMAGE_SIZE), Arrays.asList("imageWidth")));
		} else if (! validH) {
			LOGGER.warn("Failing execution due to invalid image size, WxH: {}x{}", w, h);
			return getResponse(
					new BadRequestError(BAD_REQUEST, 
							String.format("Invalid imageHeight {%d}, value must be in the range [%d..%d]",
									h,MIN_IMAGE_SIZE,MAX_IMAGE_SIZE), Arrays.asList("imageHeight")));
		}
		
		return null;
	}
	
	private static boolean isValidSize(int size) {
		return ! (size < MIN_IMAGE_SIZE || size > MAX_IMAGE_SIZE);
	}
}
