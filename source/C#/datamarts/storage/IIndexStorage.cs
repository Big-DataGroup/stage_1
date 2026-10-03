using System.Collections.Generic;

namespace BigDataPipeline.Datamarts.Storage
{

    public interface IIndexStorage
    {
        void Save(Dictionary<string, List<int>> index);
    }
}