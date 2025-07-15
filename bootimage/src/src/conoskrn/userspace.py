class SysCaller:
    def call(self, syscall: int, *args):
        raise NotImplemented

def main(syscaller: 'SysCaller'):
    import sys
    from importlib.abc import MetaPathFinder

    class SecurityError(Exception):
        pass

    copy = sys.meta_path.copy()

    class BlockInspect(MetaPathFinder):
        def find_spec(self, fullname, path, target=None):
            if fullname == "inspect" or fullname.startswith("inspect."):
                raise ImportError("Access to 'inspect' is blocked")

            for finder in copy:
                spec = finder.find_spec(fullname, path, target)
                if spec is not None:
                    return spec

            return None

    # Insert at the start of meta_path
    sys.meta_path = [BlockInspect()]
