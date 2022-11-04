from enum import Enum
from enum import unique


@unique
class TaskStatus(Enum):
    PENDING = 'Pending'
    RUNNING = 'Running'
    Succeeded = 'Succeeded'
    FAILED = 'Failed'
    CANCELLING = 'Cancelling'
    CANCELLED = 'Cancelled'
