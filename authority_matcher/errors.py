class MatcherError(Exception):
    """A safe public error; never contains credentials or upstream response bodies."""

    def __init__(self, code, message):
        super().__init__(message)
        self.code = code
        self.message = message
