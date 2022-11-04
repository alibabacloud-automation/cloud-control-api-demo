
/**
 * @author bzran
 * @description 异步任务的状态枚举
 */
public enum TaskStatusEnum {
    PENDING("Pending", "排队中"),
    RUNNING("Running","进行中"),
    Succeeded("Succeeded","执行成功"),
    FAILED("Failed","执行失败"),
    CANCELLING("Cancelling","取消中"),
    CANCELLED("Cancelled","已取消");

    private final String value;
    private final String description;

    TaskStatusEnum(String value,String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
