package com.phantomcall.app.shell;

interface IUserService {
    String exec(String cmd);
    void destroy();
}