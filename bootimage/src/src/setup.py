import os
import shutil
import zipfile
from os import PathLike

SYS_LIBS = [
    "libgl",
    "libstd",
]

SYS_APPS = [
    "mshell",
    "logon",
]


#-----------------
# INTERNAL STUFFS
#-----------------

def create_lib(name: str, out_dir: str):
    path = f"{name}/"
    if not os.path.exists(path):
        return

    if not os.path.exists(out_dir):
        os.makedirs(out_dir, exist_ok=True)

    ziph = os.path.join(out_dir, f"{name}.dlx")
    zf = zipfile.PyZipFile(ziph, 'w', zipfile.ZIP_LZMA)
    for root, dirs, files in os.walk(path):
        for file in files:
            file_path = os.path.join(root, file)
            zip_path = os.path.relpath(file_path, path)
            with zf.open(zip_path, 'w') as f:
                f.write(open(file_path, 'rb').read())
                f.close()

    zf.close()


def create_exec(name: str, out_dir: str):
    path = f"{name}/"
    if not os.path.exists(path):
        return

    if not os.path.exists(out_dir):
        os.makedirs(out_dir, exist_ok=True)

    ziph = os.path.join(out_dir, f"{name}.apx")
    zf = zipfile.PyZipFile(ziph, 'w', zipfile.ZIP_LZMA)
    for root, dirs, files in os.walk(path):
        for file in files:
            file_path = os.path.join(root, file)
            zip_path = os.path.relpath(file_path, path)
            with zf.open(zip_path, 'w') as f:
                f.write(open(file_path, 'rb').read())
                f.close()

    zf.close()


def main():
    shutil.rmtree('../dist')
    shutil.rmtree('../build')

    for lib in SYS_LIBS:
        create_lib(lib, "../build/libs")

    for lib in SYS_LIBS:
        create_exec(lib, "../build/libs")

    shutil.copyfile("main.py", "../build/cd/Boot/main.py")

    for lib in SYS_LIBS:
        shutil.copyfile(f"../build/libs/{lib}.dlx", f"../build/cd/System/{lib}.dlx")

    for lib in SYS_APPS:
        shutil.copyfile(f"../build/libs/{lib}.dlx", f"../build/cd/System/{lib}.dlx")

if __name__ == '__main__':
    main()