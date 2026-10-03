from abc import ABC, abstractmethod


class IndexStorage(ABC):
    @abstractmethod
    def save(self, index: dict):
        pass