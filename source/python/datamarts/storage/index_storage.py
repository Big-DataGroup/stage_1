from abc import ABC, abstractmethod


class IndexStorage(ABC):
    """
    Estrategia de persistencia para el índice invertido (patrón Strategy).
    Cada implementación decide CÓMO se guarda físicamente el índice
    (JSON monolítico, MongoDB, jerarquía de carpetas...), sin que
    InvertedIndex tenga que conocer los detalles.
    """

    @abstractmethod
    def save(self, index: dict):
        """
        Persiste el índice completo.

        :param index: mapa término -> lista de IDs de libros donde aparece
        """
        pass