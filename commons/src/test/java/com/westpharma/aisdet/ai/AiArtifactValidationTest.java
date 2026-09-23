package com.westpharma.aisdet.ai;

import com.westpharma.aisdet.reporting.ExtentTestListener;
import com.westpharma.aisdet.reporting.ExtentTestManager;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Bonus gate test: validates latest AI raw artifact and promotes to validated/.
 */
@Listeners(ExtentTestListener.class)
public class AiArtifactValidationTest {

    @Test(description = "Validate AI-generated test data before framework consumption")
    public void validateAiArtifactBeforeConsumption() throws Exception {
        Path validated = AiArtifactValidator.validateLatestRawArtifact();
        ExtentTestManager.logPass("Validated AI artifact at " + validated);
        org.testng.Assert.assertTrue(Files.exists(validated), "validated file must exist");
    }
}
