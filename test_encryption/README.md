# Test_encryption

This project shows how the prediction services can be extended with capability to serve encrypted models. The necessary steps required to run it (compared to models without encryption):
1. Encryption of models requires to have an implementation of the `EncryptionSpecification` interface that is also exposed as a java service, this extension is not freely available - please contact [ArosBio](https://arosbio.com) if you wish to purchase one. So firstly you have to have this extension, and as it is not available directly from Maven Central repository you have to create a local maven repository. To set this up you can look at (and modify) the [add_encrypt_jar_to_local_repo.sh](add_encrypt_jar_to_local_repo.sh) script which creates a "maven project" which can be pulled in as a dependency, note that the `artifactId`, `groupId` etc must line up with the dependency you then include in your `pom.xml` file. 
2. The [parent module](../README.md) has a profile that, if activated, includes an additional dependency for the given `groupId`, `artifactId` and `version` as well as an additional *local* repository which was set up using the script in the previous step. This profile is called `encrypt` and is thus activated by `mvn ... -P encrypt`, where `"..."` should be the Maven command that you wish to run.
3. Injecting the encryption key can be performed in 2 ways:
    - Give the key directly in plain text in either an environment variable or jvm setting called `ENCRYPTION_KEY`. **Note:** this key is assumed to be a base64 encoded `byte[]`, similarly to what you get when e.g. generating a textual key using the `generate-key` program from CPSign CLI (i.e. not giving the `-f|--file` parameter). 
    - Alternatively, give the path/URI to a file containing the encryption key using either an environment variable or jvm setting called `ENCRYPTION_KEY_FILE`. This file will be read fully and not converted in any way and be enterpreted as the raw `byte[]` key. This should be the un-altered key from e.g. `generate-key` where you specify the `-f|--file` parameter.
4. For simplicity we have given a script that shows how to start up a CP Classifier prediction service, this model was generated by the class [TestGenerateModel.java](src/test/java/TestGenerateModel.java) and saves the encrypted file in `"src/test/resources/encryptedACPmodel.jar"` as well as prints the generated encryption key that is needed to decrypt it. The CP classifier service can then be launched using the [start_test_server.sh](start_test_server.sh) script which you will have to update the encryption key in. Note that this script **both** sets the encryption key as environment variable as well as passes it as a jvm property on the final line which starts the test server, you will only need to supply it in one of these places, or use the `ENCRYPTION_KEY_FILE` instead (but then have to handle injection of the file as well). Once you started up the test server you should be able to access the Swagger UI at `http://localhost:8080/`.