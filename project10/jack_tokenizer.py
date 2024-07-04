# pylint: disable=missing-docstring, line-too-long
import re

COMMENT_PATTERN = re.compile(r"(//.*)|(/\*([^*]|[\r\n]|(\*+([^*/]|[\r\n])))*\*+/)")
KEYWORD_PATTERN = re.compile(
    r"^\s*(class|constructor|function|method|field|static|var|int|char|boolean|void|true|false|null|this|let|do|if|else|while|return)\b"
)
SYMBOL_PATTERN = re.compile(r"^\s*([{}()[\].,;+\-*/&|<>=~])")
INT_PATTERN = re.compile(r"^\s*(\d+)")
STR_PATTERN = re.compile(r'^\s*"([^"]*)"')
IDENTIFIER_PATTERN = re.compile(r"^\s*([a-zA-Z_]\w*)")


class JackTokenizer:
    TYPE_KEYWORD = 0
    TYPE_SYMBOL = 1
    TYPE_INT = 2
    TYPE_STR = 3
    TYPE_IDENT = 4

    def __init__(self, input_file_name):
        with open(input_file_name, "r", encoding="utf-8") as f:
            self._code = re.sub(
                COMMENT_PATTERN, "", f.read()
            ).strip()  # Remove comments
        self._token_type = None
        self._current_token = None

    def has_more_tokens(self):
        return bool(self._code.strip())

    def token_type(self):
        return self._token_type

    def keyword(self):
        return self._current_token

    def symbol(self):
        return self._current_token

    def identifier(self):
        return self._current_token

    def int_val(self):
        return int(self._current_token)

    def string_val(self):
        return self._current_token

    def advance(self):
        patterns = [
            (KEYWORD_PATTERN, self.TYPE_KEYWORD),
            (SYMBOL_PATTERN, self.TYPE_SYMBOL),
            (INT_PATTERN, self.TYPE_INT),
            (STR_PATTERN, self.TYPE_STR),
            (IDENTIFIER_PATTERN, self.TYPE_IDENT),
        ]

        for pattern, token_type in patterns:
            match = pattern.match(self._code)
            if match:
                self._current_token = match.group(1)
                self._code = self._code[match.end() :].strip()
                self._token_type = token_type
                break
