from dataclasses import dataclass

@dataclass
class BookMetadata:
    bookId: int
    title: str
    author: str
    language: str