package gitlet;
// Blob.java
import java.io.Serializable;

public class Blob implements Serializable {
    private byte[] content;

    public Blob(byte[] content) {
        this.content = content;
    }

    public byte[] getContent() {
        return content;
    }
}

