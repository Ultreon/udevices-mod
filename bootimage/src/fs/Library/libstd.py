from typing import Any


def fstat(fd):
    """
    Get file status information for a file descriptor.

    Args:
        fd (int): File descriptor.

    Returns:
        tuple: A tuple containing the file status information.
    """



# noinspection PyPep8Naming
class Gpu:
    def __init__(self):
        self.gl11: Any
        self.gl12: Any
        self.gl13: Any
        self.gl14: Any
        self.gl15: Any
        self.gl20: Any
        self.gl21: Any
        self.gl30: Any
        self.gl31: Any
        self.gl32: Any
        self.gl33: Any
        self.gl40: Any
        self.gl41: Any

    def getWidth(self) -> int:
        # Native API
        raise NotImplemented()

    def getHeight(self) -> int:
        # Native API
        raise NotImplemented()

    def createTexture(self, data: bytes) -> int:
        # Native API
        raise NotImplemented()

    def drawTexture(self, texture: int, x: int, y: int, width: int, height: int):
        # Native API
        raise NotImplemented()

    def deleteTexture(self, texture: int):
        # Native API
        raise NotImplemented()

    def fill(self, x: int, y: int, width: int, height: int, color: int):
        # Native API
        raise NotImplemented()

    def rect(self, x: int, y: int, width: int, height: int, color: int):
        # Native API
        raise NotImplemented()

    def clear(self, color: int):
        # Native API
        raise NotImplemented()


# noinspection PyShadowingNames,PyUnusedLocal
def syscall(syscall: str, *args) -> Any:
    raise NotImplemented  # The kernel overrides this

def get_gl() -> 'GL':
    return syscall("gl", "call")


# noinspection PyPep8Naming
class GL:
    def __init__(self):
        self.gl11: Any
        self.gl12: Any
        self.gl13: Any
        self.gl14: Any
        self.gl15: Any
        self.gl20: Any
        self.gl21: Any
        self.gl30: Any
        self.gl31: Any
        self.gl32: Any
        self.gl33: Any
        self.gl40: Any
        self.gl41: Any

        raise NotImplemented

    def getWidth(self) -> int:
        raise NotImplemented

    def getHeight(self) -> int:
        raise NotImplemented

    def createTexture(self, filename: str) -> int:
        raise NotImplemented

    def drawTexture(self, texture: int, x: int, y: int, width: int, height: int):
        raise NotImplemented

    def deleteTexture(self, texture: int):
        raise NotImplemented

    def fill(self, x: int, y: int, width: int, height: int, color: int):
        raise NotImplemented

    def rect(self, x: int, y: int, width: int, height: int, color: int):
        raise NotImplemented

    def clear(self, color: int):
        raise NotImplemented

    def getError(self) -> str:
        raise NotImplemented

    def getErrno(self) -> int:
        raise NotImplemented

    def flip(self):
        raise NotImplemented
