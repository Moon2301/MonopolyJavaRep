package com.game.monopoly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @GetMapping("/home")
    public String mainMenu() {
        return "main-menu";
    }

    @GetMapping("/profile")
    public String profile() {
        return "home/profile";
    }

    @GetMapping("/friends")
    public String friends() {
        return "home/friends";
    }

    @GetMapping("/game-history")
    public String gameHistory() {
        return "home/game-history";
    }

    @GetMapping("/private-table")
    public String privateTable() {
        return "private-table";
    }

    @GetMapping("/game-board")
    public String gameBoard() {
        return "game-board";
    }

    @GetMapping("/map-editor")
    public String mapEditor() {
        return "redirect:/home";
    }

    @GetMapping("/tournament")
    public String tournament() {
        return "redirect:/home";
    }

    @GetMapping("/tutorial")
    public String tutorial() {
        return "redirect:/home";
    }

    @GetMapping("/shop")
    public String shop() {
        return "shop";
    }

    @GetMapping("/topup")
    public String topup() {
        return "home/topup";
    }

    @GetMapping("/play-vs-ai")
    public String playVsAi() {
        return "home/play-vs-ai";
    }

    @GetMapping("/admin")
    public String adminPage() {
        return "admin/index";
    }
}
