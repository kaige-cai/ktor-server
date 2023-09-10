package ok.plugins

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.webjars.*
import kotlinx.serialization.Serializable
import ok.model.User
import java.io.File

fun Application.configureRouting() {
    install(AutoHeadResponse)
    install(DoubleReceive)
    install(Resources)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    install(Webjars) {
        path = "/webjars" //defaults to /webjars
    }

    val users = mutableListOf<User>()

    // Load user data from a JSON file (if it exists)
    val dataFile = File("user_data.json")
    if (dataFile.exists()) {
        users.addAll(dataFile.readLines().map { line ->
            val (username, password) = line.split(",")
            User(username, password)
        })
    }

    routing {
        get("/") {
            call.respondText("OK")
        }

        post("/register") {
            val newUser = call.receive<User>()
            if (users.any { it.username == newUser.username }) {
                call.respond(HttpStatusCode.BadRequest, "Username already exists")
            } else {
                users.add(newUser)
                dataFile.appendText("${newUser.username},${newUser.password}\n")
                call.respond(HttpStatusCode.OK, "Registration successful")
            }
        }

        post("/login") {
            val loginRequest = call.receive<User>()
            val matchingUser =
                users.find { it.username == loginRequest.username && it.password == loginRequest.password }
            if (matchingUser != null) {
                call.respond(HttpStatusCode.OK, "Login successful")
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Login failed")
            }
        }

        post("/double-receive") {
            val first = call.receiveText()
            val theSame = call.receiveText()
            call.respondText("$first $theSame")
        }

        get<Articles> { article ->
            // Get all articles ...
            call.respond("List of articles sorted starting from ${article.sort}")
        }
        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }
        get("/webjars") {
            call.respondText("<script src='/webjars/jquery/jquery.js'></script>", ContentType.Text.Html)
        }
    }
}

@Serializable
@Resource("/articles")
class Articles(val sort: String? = "new")
