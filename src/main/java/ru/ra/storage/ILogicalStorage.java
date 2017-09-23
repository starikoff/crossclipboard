package ru.ra.storage;

import java.util.List;

import javax.annotation.Nonnull;

import ru.ra.AuthInfo;
import ru.ra.errors.ContentReadException;
import ru.ra.errors.ContentTooLargeException;
import ru.ra.errors.ContentWriteException;
import ru.ra.storage.INote.INoteCoord;

public interface ILogicalStorage {
    @Nonnull
    List<INote> get(AuthInfo auth) throws ContentReadException;

    void remove(INoteCoord noteCoord) throws ContentWriteException;

    INote addNote(AuthInfo auth, String content) throws ContentReadException,
            ContentWriteException, ContentTooLargeException;

    ILink makeLink(INoteCoord noteCoord, String title, String faviconUrl)
            throws ContentReadException, ContentWriteException,
            ContentTooLargeException;

    INote get(INoteCoord noteCoord) throws ContentReadException;
}
