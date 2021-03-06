package org.mule.modules.caas.cli.backup;

import org.apache.commons.lang3.StringUtils;
import org.mule.modules.caas.cli.common.ApiCallValidator;
import org.mule.modules.caas.cli.config.CliConfig;
import org.slf4j.Logger;

public class BackupValidator extends ApiCallValidator {

    String backupDir;

    public BackupValidator(String backupDir) {
        this.backupDir = backupDir;
    }

    @Override
    public boolean isValid(CliConfig config, Logger loggerToUse) {
        if (!super.isValid(config, loggerToUse)) {
            return false;
        }

        if (StringUtils.isEmpty(config.getBackupsDirectory()) && StringUtils.isEmpty(backupDir)) {
            loggerToUse.error("Configuration does not specify backup directory");
            return false;
        }

        if (config.isEncryptionEnabled()) {
            if (config.getClientEncryptionKeyStore() == null) {
                loggerToUse.error("Encryption keystore not configured in settings.");
                return false;
            }

            if (config.getWrapKey() == null) {
                loggerToUse.error("Wrapping key is not configured in settings.");
                return false;
            }

            if (config.getMacKey() == null) {
                loggerToUse.error("Mac key is not configured in settings.");
                return false;
            }
        }

        return true;
    }
}
