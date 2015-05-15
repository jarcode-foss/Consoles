-- Written by Jarcode

require("list")

Server {
    -- strings
    channel,

    -- channel
    ch,

    -- tracked clients
    clients,

    -- functions
    broadcast, send = send, update = update, addClient = addClient
}
Server.__index = Server

function Server.new(channel)
    local self = setmetatable({}, Server)
    self.channel = channel
    self.ch = registerChannel(channel)
    self.clients = List.new()
    return self
end

function Server:send(message)
    message = message:gsub("\0", "?")
end

function Server:handle(tbl)
    if (tbl[1] == "0") then
        self:addClient(tbl[2])
    elseif (tbl[1] == "1") then
        
    end
end

function Server:update()
    local msg
    repeat
        msg = self.ch:poll()
        local spl = split(msg, "\0")
        self:handle(spl);
    until msg ~= nil
end

function Server:addClient(hostname)
    self.clients:append(hostname);
end