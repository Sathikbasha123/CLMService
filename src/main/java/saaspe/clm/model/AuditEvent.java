package saaspe.clm.model;

import java.util.List;

import lombok.Data;

@Data
public class AuditEvent {

	private List<EventFieldsList> auditEvents;

}
