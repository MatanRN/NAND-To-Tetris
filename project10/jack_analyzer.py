# pylint: disable=missing-docstring
import glob
import os
import sys

from typing import List
from compilation_engine import CompilationEngine


def files_to_handle(input_path: str) -> List[str]:
    if not os.path.isdir(input_path):
        return [input_path]
    return glob.glob(os.path.join(input_path, "*.jack"))


def main():
    if len(sys.argv) != 2:
        print("Usage: ./JackAnalyzer <file.jack|dir>")
        return

    if not os.path.exists(sys.argv[1]):
        print(f"Error: {sys.argv[1]} does not exist")
        return

    files = files_to_handle(sys.argv[1])
    for file_name in files:
        output_file = file_name.replace(".jack", ".xml")
        CompilationEngine(file_name, output_file).compile_class()


if __name__ == "__main__":
    main()
