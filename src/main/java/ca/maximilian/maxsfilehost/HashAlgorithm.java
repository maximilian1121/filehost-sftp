package ca.maximilian.maxsfilehost;

import lombok.Getter;

@Getter
public enum HashAlgorithm {
    MD2("MD2"),
    MD5("MD5"),
    SHA_1("SHA-1"),
    SHA_224("SHA-224"),
    SHA_256("SHA-256"),
    SHA_384("SHA-384"),
    SHA_512("SHA-512"),
    SHA_512_224("SHA-512/224"),
    SHA_512_256("SHA-512/256"),
    SHA3_224("SHA3-224"),
    SHA3_256("SHA3-256"),
    SHA3_384("SHA3-384"),
    SHA3_512("SHA3-512");

    private final String algorithmName;

    HashAlgorithm(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public static HashAlgorithm fromString(String source) {
        if (source == null) {
            return null;
        }
        for (HashAlgorithm algo : HashAlgorithm.values()) {
            if (algo.name().equalsIgnoreCase(source) ||
                    algo.getAlgorithmName().equalsIgnoreCase(source)) {
                return algo;
            }
        }
        throw new IllegalArgumentException("Unknown hash algorithm: " + source);
    }
}
