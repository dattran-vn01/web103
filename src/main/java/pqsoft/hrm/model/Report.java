package pqsoft.hrm.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Getter
@Setter
@Entity(name = "report")
public class Report {
	@Id
	@Column(name = "report_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	private String title;
	private String description;
	private Date date;

	@ManyToOne
	@JoinColumn(name = "creator_id")
	private Employee creator;

	@ManyToOne
	@JoinColumn(name = "task_id")
	private Task task;

	@Transient
	public String getTaskDisplay() {
		if(Objects.nonNull(task)) {
			return task.getTaskName();
		}
		return "";
	}
}
