package com.example.txl.tool.design.pattern.composite;

/**
 * Copyright (c) 2019, 唐小陆 All rights reserved.
 * author：txl
 * date：2019/5/28
 * description：
 */
public class Client {
    public static void main(String[] args){
        //根节点
        Composite root = new Composite( "Root" );

        Composite branch1 = new Composite( "branch1" );
        Composite branch2 = new Composite( "branch2" );

        Leaf leaf1 = new Leaf( "leaf1" );
        Leaf leaf2 = new Leaf( "leaf2" );

        branch1.addChild( leaf1 );
        branch2.addChild( leaf2 );

        root.addChild( branch1 );
        root.addChild( branch2 );

        root.doSomething();
    }
}
