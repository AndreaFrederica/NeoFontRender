package neofontrender.api.config;

/** Factory for TOML configuration files following Neo Font Render's conventions. */
public final class NfrConfigApi {
    private NfrConfigApi() {}

    public static Builder builder(String ownerId) {
        return new Builder(ownerId);
    }

    public static final class Builder {
        private final String ownerId;
        private NfrConfigStorage storage = NfrConfigStorage.INDEPENDENT;
        private String fileName;

        private Builder(String ownerId) {
            this.ownerId = NfrConfigFile.validateId(ownerId);
            this.fileName = this.ownerId + ".toml";
        }

        public Builder storage(NfrConfigStorage storage) {
            if (storage == null) throw new IllegalArgumentException("storage must not be null");
            this.storage = storage;
            return this;
        }

        /** File name used only by {@link NfrConfigStorage#INDEPENDENT}. */
        public Builder fileName(String fileName) {
            this.fileName = NfrConfigFile.validateFileName(fileName);
            return this;
        }

        public NfrConfigFile open() {
            return new NfrConfigFile(ownerId, storage, fileName);
        }
    }
}
