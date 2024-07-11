package saaspe.clm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import saaspe.clm.document.CreateTemplate;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TemplateListPagination {

    private Long total;
    private List<CreateTemplate> records;
}
