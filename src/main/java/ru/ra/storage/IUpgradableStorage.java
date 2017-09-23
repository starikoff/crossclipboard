package ru.ra.storage;

import java.util.List;

public interface IUpgradableStorage<S extends IUpgradableStorage<S>> extends IContentStorage {
    void upgrade(List<? extends IStorageUpgrade<S>> upgrades) 
            throws UpgradeException;
}
