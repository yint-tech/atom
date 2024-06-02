package cn.iinti.atom.service.base.dbcache;

public class DbWrapper<M, E> {

    public M m;

    public E e;

    public DbWrapper(M m) {
        this.m = m;
    }
}
