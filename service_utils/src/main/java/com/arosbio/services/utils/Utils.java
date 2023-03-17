package com.arosbio.services.utils;


import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.imageio.ImageIO;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

import com.arosbio.api.model.BadRequestError;
import com.arosbio.api.model.ErrorResponse;

public class Utils {
	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Utils.class);
	private static final int MAX_NUM_STACK_TO_LOGG = 10;
	
	public static final String PNG_MEDIA_TYPE = "image/png";
	public static final String DEFAULT_MODEL_PATH = "/var/lib/jetty/model.jar";
	public static final int DEFAULT_IMAGE_WH = 600;
	public static final int MIN_IMAGE_SIZE = 100;
	public static final int MAX_IMAGE_SIZE = 5000;

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
