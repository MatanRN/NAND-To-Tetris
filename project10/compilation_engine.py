# pylint: disable=missing-docstring
from jack_tokenizer import JackTokenizer

UNARY_OP = ["-", "~"]
OP = ["+", "-", "*", "/", "&", "|", "<", ">", "="]


def xml_element(tag):
    def decorator(func):
        def wrapper(self, *args, **kwargs):
            content = func(self, *args, **kwargs)
            if tag == "symbol" and content in {"<", ">", "&"}:
                content = (
                    content.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                )
            self.write(f"<{tag}> {content} </{tag}>\n")

        return wrapper

    return decorator


def xml_parent(tag):
    def decorator(func):
        def wrapper(self, *args, **kwargs):
            self.write(f"<{tag}>\n")
            func(self, *args, **kwargs)
            self.write(f"</{tag}>\n")

        return wrapper

    return decorator


class CompilationEngine:
    def __init__(self, input_file_name: str, output_file_name: str):
        self.output = open(output_file_name, "w+", encoding="utf-8")
        self.t = JackTokenizer(input_file_name)

    def write(self, code: str) -> None:
        self.output.write(code)

    def advance(self) -> None:
        self.t.advance()

    @xml_element("keyword")
    def write_keyword(self):
        return self.t.keyword()

    @xml_parent("term")
    def _write_keyword_term(self):
        self.write_keyword()

    @xml_element("symbol")
    def write_symbol(self):
        return self.t.symbol()

    @xml_parent("term")
    @xml_element("integerConstant")
    def write_int(self):
        return self.t.int_val()

    @xml_parent("term")
    @xml_element("stringConstant")
    def write_str(self):
        return self.t.string_val()

    @xml_element("identifier")
    def write_identifier(self):
        return self.t.identifier()

    def _write_keyword_or_identifier(self):
        if self.t.token_type() == self.t.TYPE_KEYWORD:
            self.write_keyword()
        else:
            self.write_identifier()

    def _write_type_and_var_name(self):
        self._write_keyword_or_identifier()
        self.advance()

        while True:
            self.write_identifier()
            self.advance()

            if self._is_symbol() and self.t.symbol() != ",":
                break

            self.write_symbol()
            self.advance()

    @xml_parent("class")
    def _write_class(self):
        self.write_keyword()  # class
        self.advance()
        self.write_identifier()  # className
        self.advance()
        self.write_symbol()  # {
        self.advance()

        while self.t.keyword() in {"static", "field"}:
            self.compile_class_var_dec()

        while self.t.keyword() in {"constructor", "function", "method"}:
            self.compile_subroutine_dec()

        self.write_symbol()  # }

    def compile_class(self):
        if self.t.has_more_tokens():
            self.advance()
            self._write_class()
            self.output.close()

    @xml_parent("classVarDec")
    def compile_class_var_dec(self):
        self.write_keyword()
        self.advance()
        self._write_type_and_var_name()
        self.write_symbol()
        self.advance()

    @xml_parent("subroutineDec")
    def compile_subroutine_dec(self):
        self.write_keyword()
        self.advance()
        self._write_keyword_or_identifier()
        self.advance()
        self.write_identifier()  # subroutineName
        self.advance()
        self.write_symbol()  # (
        self.advance()
        self.compile_parameter_list()
        self.write_symbol()  # )
        self.advance()
        self.compile_subroutine_body()

    @xml_parent("parameterList")
    def compile_parameter_list(self):
        if self.t.token_type() != self.t.TYPE_SYMBOL and self.t.symbol() != ")":
            while True:
                self._write_keyword_or_identifier()
                self.advance()
                self.write_identifier()
                self.advance()
                if self._is_symbol() and self.t.symbol() != ",":
                    break
                self.write_symbol()
                self.advance()

    @xml_parent("subroutineBody")
    def compile_subroutine_body(self):
        self.write_symbol()  # {
        self.advance()
        while self.t.token_type() == self.t.TYPE_KEYWORD and self.t.keyword() == "var":
            self.compile_var_dec()
        self.compile_statements()
        self.write_symbol()  # }
        self.advance()

    @xml_parent("varDec")
    def compile_var_dec(self):
        self.write_keyword()  # var
        self.advance()
        self._write_type_and_var_name()
        self.write_symbol()  # ;
        self.advance()

    @xml_parent("statements")
    def compile_statements(self):
        while self.t.token_type() == self.t.TYPE_KEYWORD:
            if self.t.keyword() == "let":
                self.compile_let()
            elif self.t.keyword() == "if":
                self.compile_if()
            elif self.t.keyword() == "while":
                self.compile_while()
            elif self.t.keyword() == "do":
                self.compile_do()
            elif self.t.keyword() == "return":
                self.compile_return()

    @xml_parent("letStatement")
    def compile_let(self):
        self.write_keyword()  # let
        self.advance()
        self.write_identifier()  # varName
        self.advance()

        # if varName['expression']
        if self._is_symbol() and self.t.symbol() == "[":
            self.write_symbol()  # [
            self.advance()
            self.compile_expression()
            self.write_symbol()  # ]
            self.advance()

        self.write_symbol()  # =
        self.advance()
        self.compile_expression()
        self.write_symbol()  # ;
        self.advance()

    @xml_parent("ifStatement")
    def compile_if(self):
        self.write_keyword()  # if
        self.advance()
        self.write_symbol()  # (
        self.advance()
        self.compile_expression()
        self.write_symbol()  # )
        self.advance()
        self.write_symbol()  # {
        self.advance()
        self.compile_statements()
        self.write_symbol()  # }
        self.advance()

        if self.t.token_type() == self.t.TYPE_KEYWORD and self.t.keyword() == "else":
            self.write_keyword()  # else
            self.advance()
            self.write_symbol()  # {
            self.advance()
            self.compile_statements()
            self.write_symbol()  # }
            self.advance()

    @xml_parent("whileStatement")
    def compile_while(self):
        self.write_keyword()  # while
        self.advance()
        self.write_symbol()  # (
        self.advance()
        self.compile_expression()
        self.write_symbol()  # )
        self.advance()
        self.write_symbol()  # {
        self.advance()
        self.compile_statements()
        self.write_symbol()  # }
        self.advance()

    @xml_parent("doStatement")
    def compile_do(self):
        self.write_keyword()  # do
        self.advance()
        self.write_identifier()
        self.advance()

        # if subroutineName(expressionList)
        if self._is_symbol() and self.t.symbol() == "(":
            self.compile_expression_list()

        # if foo.bar or Foo.bar
        elif self._is_symbol() and self.t.symbol() == ".":
            self.write_symbol()  # .
            self.advance()
            self.write_identifier()
            self.advance()
            self.compile_expression_list()

        self.write_symbol()  # ;
        self.advance()

    @xml_parent("returnStatement")
    def compile_return(self):
        self.write_keyword()  # return
        self.advance()
        if self.t.symbol() != ";":
            self.compile_expression()
        self.write_symbol()  # ;
        self.advance()

    @xml_parent("expression")
    def compile_expression(self):
        self.compile_term()
        while self._is_symbol() and self.t.symbol() in OP:
            self.write_symbol()  # op
            self.advance()
            self.compile_term()

    def compile_term(self):
        if self.t.token_type() == self.t.TYPE_INT:
            self.write_int()
            self.advance()

        elif self.t.token_type() == self.t.TYPE_STR:
            self.write_str()
            self.advance()

        elif self.t.token_type() == self.t.TYPE_KEYWORD:
            self._write_keyword_term()
            self.advance()

        elif self.t.token_type() == self.t.TYPE_IDENT:
            self._write_subroutine_call()

        elif self._is_symbol() and self.t.symbol() == "(":
            self._write_expression()

        elif self._is_symbol() and self.t.symbol() in UNARY_OP:
            self._write_unary_op()

    @xml_parent("term")
    def _write_subroutine_call(self):
        self.write_identifier() # varName or subroutineName
        self.advance()

        # if varName[expression]
        if self._is_symbol() and self.t.symbol() == "[":
            self.write_symbol() # [
            self.advance()
            self.compile_expression()
            self.write_symbol() # ]
            self.advance()

        # if subroutineName(expressionList)
        elif self._is_symbol() and self.t.symbol() == "(":
            self.compile_expression_list()

        # if foo.bar or Foo.bar, subroutineCall
        elif self._is_symbol() and self.t.symbol() == ".":
            self.write_symbol() # .
            self.advance()
            self.write_identifier()
            self.advance()
            self.compile_expression_list()

    @xml_parent("term")
    def _write_expression(self):
        self.write_symbol()  # (
        self.advance()
        self.compile_expression()
        self.write_symbol()  # )
        self.advance()

    @xml_parent("term")
    def _write_unary_op(self):
        self.write_symbol()
        self.advance()
        self.compile_term()

    @xml_parent("expressionList")
    def _write_expression_list(self):
        count = 0
        if self.t.symbol() != ")":
            while True:
                count += 1
                self.compile_expression()
                if self._is_symbol() and self.t.symbol() != ",":
                    break
                self.write_symbol()  # ,
                self.advance()

    def compile_expression_list(self):
        self.write_symbol()  # (
        self.advance()
        self._write_expression_list()
        self.write_symbol()  # )
        self.advance()

    def _is_symbol(self) -> bool:
        return self.t.token_type() == self.t.TYPE_SYMBOL
