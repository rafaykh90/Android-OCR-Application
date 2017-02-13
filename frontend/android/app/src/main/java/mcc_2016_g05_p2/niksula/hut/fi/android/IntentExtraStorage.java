package mcc_2016_g05_p2.niksula.hut.fi.android;

import java.util.HashMap;

public class IntentExtraStorage
{
    private static IntentExtraStorage s_instance = new IntentExtraStorage();

    private int m_nextKey = 0;
    private HashMap<Integer, Object> m_storage = new HashMap<Integer, Object>();


    private static synchronized IntentExtraStorage getInstance ()
    {
        if (s_instance == null)
            s_instance = new IntentExtraStorage();
        return s_instance;
    }

    public static int storeObject (Object o)
    {
        IntentExtraStorage self = getInstance();

        int key = self.m_nextKey++;
        self.m_storage.put(key, o);
        return key;
    }
    public static void evictObject (int key)
    {
        IntentExtraStorage self = getInstance();

        self.m_storage.remove(key);
    }
    public static Object tryRetrieve (int key)
    {
        IntentExtraStorage self = getInstance();

        // getOrDefault is API level 24
        return self.m_storage.get(key);
    }
}
