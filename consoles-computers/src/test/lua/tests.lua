
-- These are tests that are ran only using the native layer (LuaN) to test
-- various scripting functionality. These cannot use most Bukkit and Consoles
-- functionality, rather instead they test basic Java <-> C <-> Lua mappings.

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

    add_tasks({
        "basic", "functions", "callback", "array", "error_handle", "objects", "ffi_test"
    })

    log("running Lua tests")
    for i=1,#tasks do
        log("running test: '" .. tasks[i][1] .. "'")
        tasks[i][2]();
    end

    return 0;
end

function log(message)
    print("L: " .. message)
end

log("reached end of main chunk")
