package saaspe.clm.document;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "CentralRepoDocument")
public class CentralRepoDocument {

    @Transient
    public static final String SEQUENCE_NAME = "CentralRepoDocumentSequence";

    private long id;
    private String repositoryName;
    private String envelopeId;

}