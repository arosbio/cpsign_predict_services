import java.io.File;
import java.net.URL;
import java.util.Base64;
import java.util.ServiceLoader;

import org.junit.Test;

import com.arosbio.chem.io.in.SDFReader;
import com.arosbio.chem.io.in.SDFile;
import com.arosbio.cheminf.ChemCPClassifier;
import com.arosbio.cheminf.io.ModelSerializer;
import com.arosbio.data.NamedLabels;
import com.arosbio.encryption.EncryptionSpecification;
import com.arosbio.ml.algorithms.svm.LinearSVC;
import com.arosbio.ml.cp.acp.ACPClassifier;
import com.arosbio.ml.cp.nonconf.classification.NegativeDistanceToHyperplaneNCM;
import com.arosbio.ml.sampling.RandomSampling;

public class TestGenerateModel {

    /*
     * generated key: 
     * Kt6gqo8lds7mVyYr0gOdpg==
     * 
     */
    @Test
    public void generateEncryptedModel() throws Exception {
        // check we can load the encryption spec implementation
        ServiceLoader<EncryptionSpecification> loader = ServiceLoader.load(EncryptionSpecification.class);
        EncryptionSpecification spec = loader.iterator().next();
        byte[] key = spec.generateRandomKey(16);
        System.err.println("key:\n"+ Base64.getEncoder().encodeToString(key));
        spec.init(key);

        File testDir = new File(new File("").getAbsolutePath(), "src/test/resources");
        File modelOut = new File(testDir, "encryptedACPmodel.jar");
        // we generate a classification model from the 
        URL trainFile = TestGenerateModel.class.getResource("/ames_126.sdf.gz");

        ChemCPClassifier model = new ChemCPClassifier(new ACPClassifier(new NegativeDistanceToHyperplaneNCM(new LinearSVC()),new RandomSampling(1, .2)));
        
        try (SDFReader molIter = new SDFile(trainFile.toURI()).getIterator();
            ){
            model.addRecords(molIter, "Ames test categorisation", new NamedLabels("mutagen", "nonmutagen"));
        }
        System.err.println(model.getDataset().getNumRecords());
        model.train();
        try (SDFReader molIter = new SDFile(trainFile.toURI()).getIterator();
            ){
            model.computePercentiles(molIter, 20); 
            }
        
        ModelSerializer.saveModel(model, modelOut, spec);
        
    }
    
}
