

def _start():
    from typing import Callable

    class SysCallback:
        """
        Callback for syscall
        """

        def __init__(self):
            pass

        def set_on_syscall(self, func: Callable[[str], None]):
            pass

    sys_callback = SysCallback()
    sys_callback.set_on_syscall(lambda syscall: print(f"SysCall: {syscall}"))
