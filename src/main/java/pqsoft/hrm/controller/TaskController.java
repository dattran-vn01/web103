package pqsoft.hrm.controller;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import pqsoft.hrm.dao.EmployeeRepository;
import pqsoft.hrm.dao.TaskRepository;
import pqsoft.hrm.dto.TaskDto;
import pqsoft.hrm.model.Task;
import pqsoft.hrm.service.TaskService;
import pqsoft.hrm.util.SecurityUtils;

@Controller
public class TaskController {
  private final TaskRepository taskRepos;
  private final EmployeeRepository employeeRepos;
  private final TaskService taskService;

  @Autowired
  public TaskController(
      TaskRepository taskRepos, EmployeeRepository employeeRepos, TaskService taskService) {
    this.taskRepos = taskRepos;
    this.employeeRepos = employeeRepos;
    this.taskService = taskService;
  }

  @GetMapping("/tasks")
  public String index(
      Model model,
      @PageableDefault(page = 0, size = 2)
          @SortDefault.SortDefaults({
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC),
            @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC)
          })
          Pageable pageable) {

    prepareDataList(model, pageable, new LinkedMultiValueMap<>());
    return "tasks";
  }

  private void prepareDataList(
      Model model,
      @SortDefault.SortDefaults({
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC),
            @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC)
          })
          @PageableDefault(page = 0, size = 2)
          Pageable pageable,
      MultiValueMap<String, String> params) {
    params.put("admin", ImmutableList.of(String.valueOf(SecurityUtils.getAdmin())));
    final Page<Task> tasks = taskService.search(pageable, params);
    model.addAttribute(
        "pageNumbers",
        IntStream.rangeClosed(1, tasks.getTotalPages()).boxed().collect(Collectors.toList()));
    model.addAttribute("tasks", tasks);
    model.addAttribute("assignees", employeeRepos.findByAdmin(0));
    model.addAttribute("admin", SecurityUtils.getAdmin());
  }

  @RequestMapping(
    value = "/tasks/search",
    method = RequestMethod.POST,
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
  )
  public String search(
      Model model,
      @PageableDefault(page = 0, size = 10)
          @SortDefault.SortDefaults({
            @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC),
            @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC)
          })
          Pageable pageable,
      @RequestBody MultiValueMap<String, String> params) {
    prepareDataList(model, pageable, params);
    return "tasks";
  }

  @DeleteMapping("/tasks/{id}")
  public String delete(@RequestParam Integer id) {
    if (SecurityUtils.getAdmin() == 1) {
      throw new IllegalArgumentException("Don't have permission to delete the task");
    }
    taskRepos.delete(id);
    return "tasks";
  }

  @PutMapping("/tasks")
  public String create(@RequestBody TaskDto input) {
    final Task task = new Task();
    task.setTaskName(input.getName());
    task.setDescription(input.getDescription());
    task.setStatus(input.getStatus());
    task.setCreatedAt(new Date());
    task.setUpdatedAt(new Date());

    int creator = SecurityUtils.getEmployeeId();
    task.setCreator(employeeRepos.findOne(creator));

    if (!CollectionUtils.isEmpty(input.getAssignees())) {
      task.setEmployees(
          StreamSupport.stream(employeeRepos.findAll(input.getAssignees()).spliterator(), false)
              .collect(Collectors.toList()));
    }

    taskRepos.save(task);
    return "tasks";
  }

  @PostMapping("/tasks")
  public String update(@RequestBody TaskDto input) {
    if (Objects.isNull(input.getTaskId())) {
      throw new IllegalArgumentException("Invalid request to update task");
    }
    Task task = taskRepos.findOne(input.getTaskId());
    Preconditions.checkArgument(Objects.nonNull(task));

    int creator = task.getCreator().getId();

    boolean isAdmin = SecurityUtils.getAdmin() == 1;
    int employeeId = SecurityUtils.getEmployeeId();
    if (employeeId != creator && !isAdmin) {
      throw new IllegalArgumentException(
          "Don't have permission to update the task because you are not task owner or admin");
    }

    task.setTaskName(input.getName());
    task.setDescription(input.getDescription());
    task.setStatus(input.getStatus());
    task.setUpdatedAt(new Date());

    if (!CollectionUtils.isEmpty(input.getAssignees())) {
      task.setEmployees(
          StreamSupport.stream(employeeRepos.findAll(input.getAssignees()).spliterator(), false)
              .collect(Collectors.toList()));
    }
    taskRepos.save(task);

    return "tasks";
  }
}
