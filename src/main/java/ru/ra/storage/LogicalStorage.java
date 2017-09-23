package ru.ra.storage;

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import ru.ra.AuthInfo;
import ru.ra.errors.ContentReadException;
import ru.ra.errors.ContentTooLargeException;
import ru.ra.errors.ContentWriteException;
import ru.ra.storage.INote.INoteCoord;

public class LogicalStorage implements ILogicalStorage {

    private final IContentStorage storage;

    public LogicalStorage(IContentStorage storage) {
        this.storage = storage;
    }

    @Nonnull
    @Override
    public List<INote> get(AuthInfo auth) throws ContentReadException {
        return storage.get(auth);
    }

    @Override
    public void remove(INoteCoord noteCoord) throws ContentWriteException {
        storage.remove(noteCoord);
    }

    @Override
    public INote addNote(AuthInfo auth, String content)
            throws ContentReadException, ContentWriteException,
            ContentTooLargeException {
        int size = storage.getContentSize(auth);
        int newSize = size + content.length();
        int sizeLimit = storage.getSizeLimit();
        if (newSize > sizeLimit) {
            throw new ContentTooLargeException(sizeLimit, newSize);
        }
        return storage.addNote(auth, content);
    }

    @Override
    public INote get(INoteCoord noteCoord) throws ContentReadException {
        return storage.get(noteCoord);
    }

    @Override
    public ILink makeLink(INoteCoord noteCoord, String title, String favicon)
            throws ContentReadException, ContentWriteException,
            ContentTooLargeException {
        int size = storage.getContentSize(noteCoord.getOwner());
        int newSize = size + StringUtils.length(title);
        int sizeLimit = storage.getSizeLimit();
        if (newSize > sizeLimit) {
            throw new ContentTooLargeException(sizeLimit, newSize);
        }
        storage.makeLink(noteCoord.getId(), title, favicon);
        return (ILink) storage.get(noteCoord);
    }
}
