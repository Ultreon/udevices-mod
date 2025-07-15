from typing import Dict, Optional

__all__ = ["Window"]

_WM: '__ForgeWM'


class Window:
    def __init__(self, title="", width=0, height=0):
        self.id = 0
        self.title = title
        self.width = width
        self.height = height
        _WM.windows[self.id] = self

    def __setattr__(self, key, value):
        if key == "id":
            raise Exception("Cannot set id")

        if key.startswith("_"):
            raise Exception("Cannot set private attribute")

        super().__setattr__(key, value)

    def __del__(self):
        _WM.destroy_window(self.id)


class WMError(Exception):
    pass


class __ForgeWM:

    def __init__(self):
        global _WM
        if _WM is not None:
            raise WMError("Only one window manager can be created")

        _WM = self
        self.id = 0
        self.windows: Dict[int, Window] = {}

    def create_window(self, title, width, height):
        window = Window(self, title, width, height)
        id =  self.id
        self.id += 1

        window.id = id
        self.windows[id] = window

        return window.id

    def destroy_window(self, win_id: int):
        del self.windows[win_id]

    def get_window(self, win_id: int):
        return self.windows[win_id]

    def __del__(self):
        global _WM
        _WM = None


_WM = __ForgeWM()
del __ForgeWM
