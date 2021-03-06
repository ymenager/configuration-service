package org.mule.modules.caas.encryption;

import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.mule.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptionDataProvider {
	
	Logger logger = LoggerFactory.getLogger(EncryptionDataProvider.class);
	
	
	private String keystoreLocation;
	private String keystorePassword;
	private String encryptionKeyAlias;
	private String encryptionKeyPassword;
	private String blockMode = "CBC";
	private String padding = "PKCS5PADDING";
	private String wrapKeyAlias;
	private String signatureKeyAlias;
	private String wrapKeyPassword;
	private String signatureKeyPassword;
	private String initVector;

	public Map<String, String> getEncryptionDetail() throws Exception {
		
		logger.debug("Call getEncrtyptionDetail");
		
		//perform all the initial setup
		if (!StringUtils.isEmpty(keystorePassword)) {
			
			//keystore could not have password.
			//we can try and assume the keystore password as key passwords
			
			if (StringUtils.isEmpty(encryptionKeyPassword)) {
				logger.info("Encryption key password not provided, using same as keystore");
				this.encryptionKeyPassword = keystorePassword;
			}
			
			if (StringUtils.isEmpty(wrapKeyPassword)) {
				logger.info("Wrapping key password not provided, using same as keystore");
				this.wrapKeyPassword = keystorePassword;
			}
			
			if (StringUtils.isEmpty(signatureKeyPassword)) {
				logger.info("Mac key password not provided, using same as keystore");
				this.signatureKeyPassword = keystorePassword;
			}
			
		}
		
		Map<String, String> encryptiondetails = new LinkedHashMap<String, String>();
		InputStream keyStore = IOUtils.getResourceAsStream(this.keystoreLocation, getClass());
		
		if (keyStore == null) {
			logger.error("Could not load file or resource {}", keystoreLocation);
			throw new RuntimeException("Keystore not found!");
		}
		
		
		KeyStore ks = KeyStore.getInstance("JCEKS");
				
		ks.load(keyStore, keystorePassword.toCharArray());
		
		
		Key encryptioKey = loadKeyOrThrow(ks, "enc-key", encryptionKeyAlias, encryptionKeyPassword);
		Key signatureKey = loadKeyOrThrow(ks, "mac-key", signatureKeyAlias, signatureKeyPassword);
		Key wrappingKey = loadKeyOrThrow(ks, "wrap-key", wrapKeyAlias, wrapKeyPassword);

		String algorithm = Base64
				.encodeBase64String((encryptioKey.getAlgorithm() + "/" + blockMode + "/" + padding).getBytes());
		Cipher cipher = Cipher.getInstance(wrappingKey.getAlgorithm());
		cipher.init(Cipher.WRAP_MODE, wrappingKey);
		byte[] encodedKey = cipher.wrap(encryptioKey);
		Mac mac = Mac.getInstance(signatureKey.getAlgorithm());
		mac.init(signatureKey);
		
		String signature = Base64.encodeBase64String(mac.doFinal(encodedKey));
		encryptiondetails.put("algorithm", algorithm);
		encryptiondetails.put("encodedKey", Base64.encodeBase64String(encodedKey));
		encryptiondetails.put("macSignature", signature);
		encryptiondetails.put("parameters", getParameter(encryptioKey));

		return encryptiondetails;
	}
	
	private Key loadKeyOrThrow(KeyStore ks, String defaultAlias, String alias, String password) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
		
		char[] pwc = password.toCharArray();
		
		Key ret = ks.getKey(alias, pwc);
		
		if (ret == null) {
			//try the default alias
			ret = ks.getKey(defaultAlias, pwc);
		}
		
		if (ret == null) {
			throw new RuntimeException("Could not locate key: " + defaultAlias + " in keystore");
		}
		
		return ret;
	}
	

	private String getParameter(Key encryptioKey) throws Exception {
		String param = ((encryptioKey.getAlgorithm() + "/" + blockMode + "/" + padding));
		Cipher cipher = Cipher.getInstance(param);
		IvParameterSpec iv = buildInitVector(cipher, encryptioKey);
		cipher.init(Cipher.ENCRYPT_MODE, encryptioKey, iv);
		return Base64.encodeBase64String(cipher.getParameters().getEncoded());
	}

	private IvParameterSpec buildInitVector(Cipher cipher, Key key) throws GeneralSecurityException {
		byte[] iv = new byte[cipher.getBlockSize()];

		byte[] keyBytes = null;

		if (initVector != null) {
			keyBytes = initVector.getBytes();
		} else {
			keyBytes = key.getEncoded();
		}

		// copy the bytes of the key
		for (int i = 0; i < iv.length; i++) {
			iv[i] = i < keyBytes.length ? keyBytes[i] : (byte) i;
		}

		return new IvParameterSpec(iv);
	}
	
	//GETTERS AND SETTERS 

	public String getKeystoreLocation() {
		return keystoreLocation;
	}

	public void setKeystoreLocation(String keystoreLocation) {
		this.keystoreLocation = keystoreLocation;
	}

	public String getKeystorePassword() {
		return keystorePassword;
	}

	public void setKeystorePassword(String keystorePassword) {
		this.keystorePassword = keystorePassword;
	}

	public String getEncryptionKeyAlias() {
		return encryptionKeyAlias;
	}

	public void setEncryptionKeyAlias(String encryptionKeyAlias) {
		this.encryptionKeyAlias = encryptionKeyAlias;
	}

	public String getEncryptionKeyPassword() {
		return encryptionKeyPassword;
	}

	public void setEncryptionKeyPassword(String encryptionKeyPassword) {
		this.encryptionKeyPassword = encryptionKeyPassword;
	}

	public String getBlockMode() {
		return blockMode;
	}

	public void setBlockMode(String blockMode) {
		this.blockMode = blockMode;
	}

	public String getPadding() {
		return padding;
	}

	public void setPadding(String padding) {
		this.padding = padding;
	}

	public String getWrapKeyAlias() {
		return wrapKeyAlias;
	}

	public void setWrapKeyAlias(String wrapKeyAlias) {
		this.wrapKeyAlias = wrapKeyAlias;
	}

	public String getSignatureKeyAlias() {
		return signatureKeyAlias;
	}

	public void setSignatureKeyAlias(String signatureKeyAlias) {
		this.signatureKeyAlias = signatureKeyAlias;
	}

	public String getWrapKeyPassword() {
		return wrapKeyPassword;
	}

	public void setWrapKeyPassword(String wrapKeyPassword) {
		this.wrapKeyPassword = wrapKeyPassword;
	}

	public String getSignatureKeyPassword() {
		return signatureKeyPassword;
	}

	public void setSignatureKeyPassword(String signatureKeypassword) {
		this.signatureKeyPassword = signatureKeypassword;
	}

	public String getInitVector() {
		return initVector;
	}

	public void setInitVector(String initVector) {
		this.initVector = initVector;
	}

}
