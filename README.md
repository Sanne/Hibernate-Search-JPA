Hibernate-Search-JPA
====================

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Hotware/Hibernate-Search-JPA?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Hibernate-Search with the JPA provider you want.

http://hibernatesearchandjpa.blogspot.de/

Introduction
============
Hibernate Search is an awesome library when you have a JPA based application and you want to add fully fletched fulltext search capabilities to your domain model. You simply add annotations to the fields you want to index and then you can generate a working Index from the JPA objects. When the database changes, the index is updated accordingly. This works just fine (TM).

Here is an example from the [Hibernate Search getting started page](http://hibernate.org/search/documentation/getting-started/)

    @Entity
    @Indexed
    public class Book {
    
      @Id
      @GeneratedValue
      private Integer id;
    
      @Field(index=Index.YES, analyze=Analyze.YES, store=Store.NO)
      private String title;
    
      @Field(index=Index.YES, analyze=Analyze.YES, store=Store.NO)
      private String subtitle;
    
      @Field(index=Index.YES, analyze=Analyze.NO, store=Store.YES)
      @DateBridge(resolution=Resolution.DAY)
      private Date publicationDate;
    
      @IndexedEmbedded
      @ManyToMany
      private Set<Author> authors = new HashSet<Author>();
      public Book() {
      }
    
      // standard getters/setters follow here
      ...
    }
    
One of the few problems it has, is that once you decide to use Hibernate Search you have to use/stick with Hibernate ORM and lose the possibility to swap the JPA provider for something along the lines of EclipseLink (switching the JPA provider - in my eyes - is one of the big benefits of using JPA), i.e. because your (new) Jave EE Container ships with it and you don't want to change it. This is due to Hibernate Search relying on Hibernate ORM specific events to update its index. These are by far more sophisticated than the ones plain JPA provides and while other JPA providers might have similar features, there is no clear specification for these.

![The current problem](http://4.bp.blogspot.com/-AMJtBIXzeSQ/VUCdkYUCMGI/AAAAAAAAALc/S8OVX9esVOQ/s1600/Hibernate-Search-With-Any-Problem-Schema.png)

The goal of Hibernate-Search-JPA is to fix this and provide an integration of Hibernate Search's Engine that works with (most) JPA providers (and for now only SQL databases):

![What we aim for](http://1.bp.blogspot.com/-U0osGoQE0DI/VUCemKbVoJI/AAAAAAAAALk/f1vt4Fln2ko/s1600/Basic%2BDesign%2B(1).png)

Examples
========

An example on how to use this project can be found in the wiki: 
https://github.com/Hotware/Hibernate-Search-JPA/wiki/How-To
