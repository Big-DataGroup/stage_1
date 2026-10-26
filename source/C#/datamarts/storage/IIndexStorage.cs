using System.Collections.Generic;

namespace BigDataPipeline.Datamarts.Storage
{
    /// <summary>
    /// Estrategia de persistencia para el índice invertido (patrón Strategy).
    /// </summary>
    public interface IIndexStorage
    {
        // En C# Map<String, List<Integer>> se traduce como Dictionary<string, List<int>>[cite: 34]
        void Save(Dictionary<string, List<int>> index);
    }
}