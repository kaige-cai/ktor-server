package ok.plugins

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.webjars.*
import ok.model.Articles
import ok.model.User
import ok.model.UserList
import java.io.File

fun Application.configureRouting() {
    install(AutoHeadResponse)
    install(DoubleReceive)
    install(Resources)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val errorMessage = "500: ${cause.localizedMessage}"
            call.respond(HttpStatusCode.InternalServerError, errorMessage)
        }
    }
    install(Webjars) {
        path = "/webjars" //defaults to /webjars
    }

    // 构建文件路径，将user_data.json保存在服务器站点目录下的/data/目录中
    val dataFilePath = File("data/user_data.json")

    val objectMapper = jacksonObjectMapper() // 创建 Jackson ObjectMapper
    val userList: UserList = if (dataFilePath.exists() && dataFilePath.length() > 0) {
        objectMapper.readValue(dataFilePath, UserList::class.java)
    } else {
        UserList(mutableListOf())
    }

    val users = userList.users.toMutableList()

    if (!dataFilePath.exists()) {
        dataFilePath.parentFile.mkdirs() // 如果文件不存在，创建data目录
        dataFilePath.createNewFile() // 创建空的user_data.json文件
    }

    if (dataFilePath.exists() && dataFilePath.length() > 0) {
        val usersFromFile: List<User> = objectMapper.readValue(
            dataFilePath,
            objectMapper.typeFactory.constructCollectionType(List::class.java, User::class.java)
        )
        users.addAll(usersFromFile)
    }

    routing {
        get("/") {
            call.respondText("OK👌")
        }

        post("/register") {
            val newUser = call.receive<User>()
            if (users.any { it.username == newUser.username }) {
                call.respond(HttpStatusCode.BadRequest, "Username already exists")
            } else {
                users.add(newUser)

                // 将用户列表写入文件
                val userList = UserList(users)
                dataFilePath.writeText(objectMapper.writeValueAsString(userList))

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
