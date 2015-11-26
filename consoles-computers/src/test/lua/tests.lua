
-- flush output automatically
io.output():setvbuf("no")

tasks = {};

function add_tasks(names)
    for i=1,#names do
        tasks[#tasks + 1] = { names[i], require(names[i]) };
    end
end

-- Any compile-time tests can be ran by this script.
function test(workingDir)
    -- search tasks directory (the working directory is inherited from the maven module)
    package.path = package.path .. ";" .. workingDir .. "/src/test/lua/tasks/?.lua"

    add_tasks({ "basic", "functions" })

    log("Running Lua tests")
    for i=1,#tasks do
        log("Running test: '" .. tasks[i][1] .. "'")
        tasks[i][2]();
    end
end

function log(message)
    print("L: " .. message)
end

log("reached end of main chunk")